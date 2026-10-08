package test.deeppathedgetypes;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.deeppathedgetypes.meta.ROuterMeta;

import static java.util.Optional.ofNullable;

/**
 * The remote nested choice - its util is generated in THIS namespace.
 * @version 0.0.0
 */
@RosettaDataType(value="ROuter", builder=ROuter.ROuterBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ROuter", model="test", builder=ROuter.ROuterBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface ROuter extends RosettaModelObject {

	ROuterMeta metaData = new ROuterMeta();

	/*********************** Getter Methods  ***********************/
	RWrap getRWrap();
	RNote getRNote();

	/*********************** Build Methods  ***********************/
	ROuter build();
	
	ROuter.ROuterBuilder toBuilder();
	
	static ROuter.ROuterBuilder builder() {
		return new ROuter.ROuterBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ROuter> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ROuter> getType() {
		return ROuter.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("RWrap"), processor, RWrap.class, getRWrap());
		processRosetta(path.newSubPath("RNote"), processor, RNote.class, getRNote());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ROuterBuilder extends ROuter, RosettaModelObjectBuilder {
		RWrap.RWrapBuilder getOrCreateRWrap();
		@Override
		RWrap.RWrapBuilder getRWrap();
		RNote.RNoteBuilder getOrCreateRNote();
		@Override
		RNote.RNoteBuilder getRNote();
		ROuter.ROuterBuilder setRWrap(RWrap _RWrap);
		ROuter.ROuterBuilder setRNote(RNote _RNote);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("RWrap"), processor, RWrap.RWrapBuilder.class, getRWrap());
			processRosetta(path.newSubPath("RNote"), processor, RNote.RNoteBuilder.class, getRNote());
		}
		

		ROuter.ROuterBuilder prune();
	}

	/*********************** Immutable Implementation of ROuter  ***********************/
	class ROuterImpl implements ROuter {
		private final RWrap rWrap;
		private final RNote rNote;
		
		protected ROuterImpl(ROuter.ROuterBuilder builder) {
			this.rWrap = ofNullable(builder.getRWrap()).map(f->f.build()).orElse(null);
			this.rNote = ofNullable(builder.getRNote()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("RWrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RWrap")
		public RWrap getRWrap() {
			return rWrap;
		}
		
		@Override
		@RosettaAttribute("RNote")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RNote")
		public RNote getRNote() {
			return rNote;
		}
		
		@Override
		public ROuter build() {
			return this;
		}
		
		@Override
		public ROuter.ROuterBuilder toBuilder() {
			ROuter.ROuterBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ROuter.ROuterBuilder builder) {
			ofNullable(getRWrap()).ifPresent(builder::setRWrap);
			ofNullable(getRNote()).ifPresent(builder::setRNote);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ROuter _that = getType().cast(o);
		
			if (!Objects.equals(rWrap, _that.getRWrap())) return false;
			if (!Objects.equals(rNote, _that.getRNote())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (rWrap != null ? rWrap.hashCode() : 0);
			_result = 31 * _result + (rNote != null ? rNote.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ROuter {" +
				"RWrap=" + this.rWrap + ", " +
				"RNote=" + this.rNote +
			'}';
		}
	}

	/*********************** Builder Implementation of ROuter  ***********************/
	class ROuterBuilderImpl implements ROuter.ROuterBuilder {
	
		protected RWrap.RWrapBuilder rWrap;
		protected RNote.RNoteBuilder rNote;
		
		@Override
		@RosettaAttribute("RWrap")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RWrap")
		public RWrap.RWrapBuilder getRWrap() {
			return rWrap;
		}
		
		@Override
		public RWrap.RWrapBuilder getOrCreateRWrap() {
			RWrap.RWrapBuilder result;
			if (rWrap!=null) {
				result = rWrap;
			}
			else {
				result = rWrap = RWrap.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("RNote")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("RNote")
		public RNote.RNoteBuilder getRNote() {
			return rNote;
		}
		
		@Override
		public RNote.RNoteBuilder getOrCreateRNote() {
			RNote.RNoteBuilder result;
			if (rNote!=null) {
				result = rNote;
			}
			else {
				result = rNote = RNote.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("RWrap")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("RWrap")
		@Override
		public ROuter.ROuterBuilder setRWrap(RWrap _rWrap) {
			this.rWrap = _rWrap == null ? null : _rWrap.toBuilder();
			return this;
		}
		
		@RosettaAttribute("RNote")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("RNote")
		@Override
		public ROuter.ROuterBuilder setRNote(RNote _rNote) {
			this.rNote = _rNote == null ? null : _rNote.toBuilder();
			return this;
		}
		
		@Override
		public ROuter build() {
			return new ROuter.ROuterImpl(this);
		}
		
		@Override
		public ROuter.ROuterBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ROuter.ROuterBuilder prune() {
			if (rWrap!=null && !rWrap.prune().hasData()) rWrap = null;
			if (rNote!=null && !rNote.prune().hasData()) rNote = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRWrap()!=null && getRWrap().hasData()) return true;
			if (getRNote()!=null && getRNote().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ROuter.ROuterBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ROuter.ROuterBuilder o = (ROuter.ROuterBuilder) other;
			
			merger.mergeRosetta(getRWrap(), o.getRWrap(), this::setRWrap);
			merger.mergeRosetta(getRNote(), o.getRNote(), this::setRNote);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ROuter _that = getType().cast(o);
		
			if (!Objects.equals(rWrap, _that.getRWrap())) return false;
			if (!Objects.equals(rNote, _that.getRNote())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (rWrap != null ? rWrap.hashCode() : 0);
			_result = 31 * _result + (rNote != null ? rNote.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ROuterBuilder {" +
				"RWrap=" + this.rWrap + ", " +
				"RNote=" + this.rNote +
			'}';
		}
	}
}
