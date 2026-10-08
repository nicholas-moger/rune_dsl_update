package chaos.s34.a2dangle;

import chaos.s34.a2dangle.h.C34Aux;
import chaos.s34.a2dangle.meta.C34MarkedMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Annotated type and attribute - bare, attribute-selecting and prefixed references.
 * @version 1.0.0
 */
@RosettaDataType(value="C34Marked", builder=C34Marked.C34MarkedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C34Marked", model="chaos", builder=C34Marked.C34MarkedBuilderImpl.class, version="1.0.0")
public interface C34Marked extends RosettaModelObject {

	C34MarkedMeta metaData = new C34MarkedMeta();

	/*********************** Getter Methods  ***********************/
	String getMid();
	C34Aux getAux();

	/*********************** Build Methods  ***********************/
	C34Marked build();
	
	C34Marked.C34MarkedBuilder toBuilder();
	
	static C34Marked.C34MarkedBuilder builder() {
		return new C34Marked.C34MarkedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C34Marked> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C34Marked> getType() {
		return C34Marked.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("mid"), String.class, getMid(), this);
		processRosetta(path.newSubPath("aux"), processor, C34Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C34MarkedBuilder extends C34Marked, RosettaModelObjectBuilder {
		C34Aux.C34AuxBuilder getOrCreateAux();
		@Override
		C34Aux.C34AuxBuilder getAux();
		C34Marked.C34MarkedBuilder setMid(String mid);
		C34Marked.C34MarkedBuilder setAux(C34Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("mid"), String.class, getMid(), this);
			processRosetta(path.newSubPath("aux"), processor, C34Aux.C34AuxBuilder.class, getAux());
		}
		

		C34Marked.C34MarkedBuilder prune();
	}

	/*********************** Immutable Implementation of C34Marked  ***********************/
	class C34MarkedImpl implements C34Marked {
		private final String mid;
		private final C34Aux aux;
		
		protected C34MarkedImpl(C34Marked.C34MarkedBuilder builder) {
			this.mid = builder.getMid();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("mid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mid")
		public String getMid() {
			return mid;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C34Aux getAux() {
			return aux;
		}
		
		@Override
		public C34Marked build() {
			return this;
		}
		
		@Override
		public C34Marked.C34MarkedBuilder toBuilder() {
			C34Marked.C34MarkedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C34Marked.C34MarkedBuilder builder) {
			ofNullable(getMid()).ifPresent(builder::setMid);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C34Marked _that = getType().cast(o);
		
			if (!Objects.equals(mid, _that.getMid())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mid != null ? mid.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C34Marked {" +
				"mid=" + this.mid + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C34Marked  ***********************/
	class C34MarkedBuilderImpl implements C34Marked.C34MarkedBuilder {
	
		protected String mid;
		protected C34Aux.C34AuxBuilder aux;
		
		@Override
		@RosettaAttribute("mid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mid")
		public String getMid() {
			return mid;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C34Aux.C34AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C34Aux.C34AuxBuilder getOrCreateAux() {
			C34Aux.C34AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C34Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("mid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("mid")
		@Override
		public C34Marked.C34MarkedBuilder setMid(String _mid) {
			this.mid = _mid == null ? null : _mid;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C34Marked.C34MarkedBuilder setAux(C34Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C34Marked build() {
			return new C34Marked.C34MarkedImpl(this);
		}
		
		@Override
		public C34Marked.C34MarkedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C34Marked.C34MarkedBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMid()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C34Marked.C34MarkedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C34Marked.C34MarkedBuilder o = (C34Marked.C34MarkedBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getMid(), o.getMid(), this::setMid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C34Marked _that = getType().cast(o);
		
			if (!Objects.equals(mid, _that.getMid())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mid != null ? mid.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C34MarkedBuilder {" +
				"mid=" + this.mid + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
