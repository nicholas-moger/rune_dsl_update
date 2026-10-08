package chaos.s27.a3hub.p1;

import chaos.s27.a3hub.p1.meta.C27RefMeta;
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
 * Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C27Ref", builder=C27Ref.C27RefBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C27Ref", model="chaos", builder=C27Ref.C27RefBuilderImpl.class, version="1.0.0")
public interface C27Ref extends RosettaModelObject {

	C27RefMeta metaData = new C27RefMeta();

	/*********************** Getter Methods  ***********************/
	String getCaption();

	/*********************** Build Methods  ***********************/
	C27Ref build();
	
	C27Ref.C27RefBuilder toBuilder();
	
	static C27Ref.C27RefBuilder builder() {
		return new C27Ref.C27RefBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C27Ref> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C27Ref> getType() {
		return C27Ref.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("caption"), String.class, getCaption(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C27RefBuilder extends C27Ref, RosettaModelObjectBuilder {
		C27Ref.C27RefBuilder setCaption(String caption);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("caption"), String.class, getCaption(), this);
		}
		

		C27Ref.C27RefBuilder prune();
	}

	/*********************** Immutable Implementation of C27Ref  ***********************/
	class C27RefImpl implements C27Ref {
		private final String caption;
		
		protected C27RefImpl(C27Ref.C27RefBuilder builder) {
			this.caption = builder.getCaption();
		}
		
		@Override
		@RosettaAttribute("caption")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("caption")
		public String getCaption() {
			return caption;
		}
		
		@Override
		public C27Ref build() {
			return this;
		}
		
		@Override
		public C27Ref.C27RefBuilder toBuilder() {
			C27Ref.C27RefBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C27Ref.C27RefBuilder builder) {
			ofNullable(getCaption()).ifPresent(builder::setCaption);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C27Ref _that = getType().cast(o);
		
			if (!Objects.equals(caption, _that.getCaption())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (caption != null ? caption.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C27Ref {" +
				"caption=" + this.caption +
			'}';
		}
	}

	/*********************** Builder Implementation of C27Ref  ***********************/
	class C27RefBuilderImpl implements C27Ref.C27RefBuilder {
	
		protected String caption;
		
		@Override
		@RosettaAttribute("caption")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("caption")
		public String getCaption() {
			return caption;
		}
		
		@RosettaAttribute("caption")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("caption")
		@Override
		public C27Ref.C27RefBuilder setCaption(String _caption) {
			this.caption = _caption == null ? null : _caption;
			return this;
		}
		
		@Override
		public C27Ref build() {
			return new C27Ref.C27RefImpl(this);
		}
		
		@Override
		public C27Ref.C27RefBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C27Ref.C27RefBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCaption()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C27Ref.C27RefBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C27Ref.C27RefBuilder o = (C27Ref.C27RefBuilder) other;
			
			
			merger.mergeBasic(getCaption(), o.getCaption(), this::setCaption);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C27Ref _that = getType().cast(o);
		
			if (!Objects.equals(caption, _that.getCaption())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (caption != null ? caption.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C27RefBuilder {" +
				"caption=" + this.caption +
			'}';
		}
	}
}
